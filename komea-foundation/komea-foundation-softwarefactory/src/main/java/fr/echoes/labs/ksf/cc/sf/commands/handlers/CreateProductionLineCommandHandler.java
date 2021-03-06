package fr.echoes.labs.ksf.cc.sf.commands.handlers;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.tocea.corolla.cqrs.annotations.CommandHandler;
import com.tocea.corolla.cqrs.gate.Gate;
import com.tocea.corolla.cqrs.handler.ICommandHandler;

import fr.echoes.labs.ksf.cc.sf.commands.CreateProductionLineCommand;
import fr.echoes.labs.ksf.cc.sf.dao.IProductionLineDAO;
import fr.echoes.labs.ksf.cc.sf.domain.ProductionLine;
import fr.echoes.labs.ksf.cc.sf.events.EventProductionLineCreated;
import fr.echoes.labs.ksf.cc.sf.exceptions.InvalidProductionLineInformationException;
import fr.echoes.labs.ksf.cc.sf.exceptions.MissingProductionLineInformationException;
import fr.echoes.labs.ksf.cc.sf.exceptions.ProductionLineAlreadyExistsException;

@CommandHandler
public class CreateProductionLineCommandHandler implements ICommandHandler<CreateProductionLineCommand, ProductionLine> {

	@Autowired
	private IProductionLineDAO productionLineDAO;
	
	@Autowired
	private Gate gate;
	
	@Override
	public ProductionLine handle(@Valid CreateProductionLineCommand command) {
		
		if (command.getProductionLine() == null) {
			throw new MissingProductionLineInformationException("No production line found");
		}
		
		ProductionLine productionLine = command.getProductionLine();
		
		if (StringUtils.isNotEmpty(productionLine.getId())) {
			throw new InvalidProductionLineInformationException("No ID expected");
		}
	
		ProductionLine alreadyExists = productionLineDAO.findByProject(productionLine.getProject());
		
		if (alreadyExists != null) {
			throw new ProductionLineAlreadyExistsException();
		}
		
		productionLineDAO.save(productionLine);
		
		gate.dispatchEvent(new EventProductionLineCreated(productionLine));
		
		return productionLine;
		
	}

}
